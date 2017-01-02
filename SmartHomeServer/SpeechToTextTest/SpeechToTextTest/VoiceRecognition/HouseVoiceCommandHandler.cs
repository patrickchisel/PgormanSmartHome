using SpeechToTextTest.HouseModel;
using SpeechToTextTest.SoundFeedback;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Speech.Recognition;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Configuration;
using HueBulbRestLibrary;
using System.IO;
using System.Speech.AudioFormat;

namespace SpeechToTextTest.VoiceRecognition
{
    public class HouseVoiceCommandHandler
    {
        private const int CommandSilenceTimeout = 5000;

        private object checkLastSpeechLock = new object();
        
        // Do not access this variable directly, it should be synchronized access through methods.
        public DateTime LastSpeechTime { get; set; }


        private object stateLock = new object();
        private bool _commandInitiated;
        public bool CommandInitiated
        {
            get
            {
                lock (stateLock)
                {
                    return _commandInitiated;
                }
            }
            set
            {
                lock (stateLock)
                {
                    _commandInitiated = value;
                }
            }
        }

        private HouseLightsCommandGrammarBuilderFactory GrammarBuilderFactory { get; set; }

        private HueBulbClientLib HueBulbClient { get; set; }

        public CancellationTokenSource speechCancellationTokenSource { get; set; }

        // TODO there is no reason I cannot detect this from hue, except for the voice part.  FK for the voice part.
        private static HouseSpec DefaultHouseSpec = new HouseSpec(new List<RoomSpec>
        {
            new RoomSpec("MASTER_BEDROOM", new List<string> { "master bedroom", "big bedroom", "suite" }, new List<string> { "1" }),
            new RoomSpec("SMALL_BATHROOM", new List<string> { "half bath" }, new List<string>()),
            new RoomSpec("BIG_BATHROOM", new List<string> { "big bathroom", "main bathroom" }, new List<string>()),
            new RoomSpec("KITCHEN", new List<string> { "kitchen", "dining room" }, new List<string>()),
            new RoomSpec("LIVING_ROOM", new List<string> { "living room", "tv room", "entrance" }, new List<string> { "3" }),
            new RoomSpec("GARAGE", new List<string> { "garage", "car port" }, new List<string>()),
            new RoomSpec("OFFICE", new List<string> { "office", "computer room" }, new List<string> { "2" }),
            new RoomSpec("HALLWAY", new List<string> { "hallway" }, new List<string>()),
            new RoomSpec("GUEST_BEDROOM", new List<string> { "guest bedroom" }, new List<string>()),
            new RoomSpec("HEDGEHOG_ROOM", new List<string> { "hedgehog room" }, new List<string>())
        });

        public HouseVoiceCommandHandler()
        {
            CommandInitiated = false;
            LastSpeechTime = DateTime.MinValue;
            GrammarBuilderFactory = new HouseLightsCommandGrammarBuilderFactory();

            var hueBaseUrl = ConfigurationManager.AppSettings["hueClientBaseUrl"];
            var hueUsername = ConfigurationManager.AppSettings["hueClientUsername"];
            HueBulbClient = new HueBulbClientLib(hueBaseUrl, hueUsername);
        }

        public Task InitiateSpeechRecognition()
        {
            speechCancellationTokenSource = new CancellationTokenSource();

            var houseSepc = DefaultHouseSpec;

            var grammarBuilder = GrammarBuilderFactory.CreateGrammarBuilder(houseSepc);
            var recognizerInfo = SpeechRecognitionEngine.InstalledRecognizers().FirstOrDefault(ri => ri.Culture.TwoLetterISOLanguageName.Equals("en"));

            var speechRecognitionTask = Task.Run(() => RunSpeechRecognizer(grammarBuilder, recognizerInfo));
            return speechRecognitionTask;
        }


        public LightActionInfo RunRecognizerOnSound(Stream soundBytes)
        {
            var grammarBuilder = GrammarBuilderFactory.CreateGrammarBuilder(DefaultHouseSpec);
            var recognizerInfo = SpeechRecognitionEngine.InstalledRecognizers().FirstOrDefault(ri => ri.Culture.TwoLetterISOLanguageName.Equals("en"));

            using (SpeechRecognitionEngine recognizer = new SpeechRecognitionEngine(recognizerInfo))
            {
                recognizer.LoadGrammar(new Grammar(grammarBuilder));
                recognizer.SetInputToWaveStream(soundBytes);
                var result = recognizer.Recognize(TimeSpan.FromSeconds(2));
                var actionInfo = GetActionInfoFromRecognitionResult(result);
                return actionInfo;
            }
        }

        private void RunSpeechRecognizer(GrammarBuilder finalGrammarBuilder, RecognizerInfo recognizerInfo)
        {
            using (SpeechRecognitionEngine recognizer = new SpeechRecognitionEngine(recognizerInfo))
            {
                //recognizer.LoadGrammar(grammar);
                recognizer.LoadGrammar(new Grammar(finalGrammarBuilder));
                recognizer.SpeechRecognized += Engine_SpeechRecognized;

                // Configure input to the speech recognizer.
                recognizer.SetInputToDefaultAudioDevice();

                // Start asynchronous, continuous speech recognition.
                recognizer.RecognizeAsync(RecognizeMode.Multiple);

                while(!speechCancellationTokenSource.Token.IsCancellationRequested)
                {
                    CheckCommandTimeout();
                }

                Console.WriteLine("Cancelling Speech Recognizer Task.");
            }
        }

        private void CheckCommandTimeout()
        {
            if (CommandInitiated && IsSpeechTimeout(DateTime.UtcNow))
            {
                Console.WriteLine("Command timeout has occurred since the last command, resetting command initiated to false.");
                CommandInitiated = false;
                ComputerFeedbackPlayer.PlayComputerTimeout();
            }
        }


        

        private void Engine_SpeechRecognized(object sender, SpeechRecognizedEventArgs e)
        {
            if (string.Equals(e.Result.Text, CommandConstants.InitiateCommandsPhrase, StringComparison.OrdinalIgnoreCase))
            {
                Console.WriteLine("Acknowledged...");
                ComputerFeedbackPlayer.PlayComputerInit();

                // TODO do I need to lock this when changing it?
                CommandInitiated = true;
            }
            else if(string.Equals(e.Result.Text, CommandConstants.CancelProgramCommand, StringComparison.OrdinalIgnoreCase))
            {
                Console.WriteLine("Program Quit Detected.");
                speechCancellationTokenSource.Cancel();
            }
            else
            {
                if(!CommandInitiated)
                {
                    return;
                }

                var semantics = e.Result.Semantics;

                if (!semantics.ContainsKey(CommandConstants.CommandSubjectSemanticKey))
                {
                    Console.WriteLine("Grammar recognized but no subject was found on which to take an action.");
                    //throw new Exception("Grammar recognized but no subject was found on which to take an action.");
                }

                var subject = e.Result.Semantics[CommandConstants.CommandSubjectSemanticKey];

                if (Equals(subject.Value, LightVoiceSubject.LightSubjectSemanticValue))
                {
                    ComputerFeedbackPlayer.PlayComputerAck();

                    if (!semantics.ContainsKey(CommandConstants.LightIdentifierSemanticKey) || !semantics.ContainsKey(CommandConstants.LightActionSemanticKey))
                    {
                        Console.WriteLine("A command with the light subject must contain a light identifier and action semantic.");
                        //throw new Exception("A command with the light subject must contain a light identifier and action semantic.");
                    }

                    var identifier = e.Result.Semantics[CommandConstants.LightIdentifierSemanticKey];
                    var action = e.Result.Semantics[CommandConstants.LightActionSemanticKey];

                    var actionInfo = new LightActionInfo(subject.Value.ToString(), action.Value.ToString(), identifier.Value.ToString());
                    Console.WriteLine("Grammer match: {0}", e.Result.Text);
                    Console.WriteLine("subject:{0}, action:{1}, identifier:{2}", actionInfo.Subject, actionInfo.Action, actionInfo.Identifier);
                    Console.WriteLine("With Confidence {0}", e.Result.Confidence);

                    // TODO passing house spec this way is cheating a bit.
                    Task.Run(async() => await ExecuteLightAction(actionInfo, DefaultHouseSpec)).Wait();
                }
                else
                {
                    Console.WriteLine("Subject value {0} was found but can not be bound to an action", subject.Value);
                    //throw new Exception(string.Format("Subject value {0} was found but can not be bound to an action", subject.Value));
                }
            }

            // Do not share a variable with the above code, this is to protect against long processing times.
            SetNewSpeechTime(DateTime.UtcNow);
        }

        private async Task ExecuteLightAction(LightActionInfo actionInfo, HouseSpec houseSpec)
        {
            bool lightOnState = actionInfo.Action.Equals(TurnOnVoiceAction.TurnOnLightsSemanticValue) ? true : false;
            var roomId = actionInfo.Identifier;

            List<string> matchingLightbulbIds;

            if(roomId.Equals(LightVoiceIdentifier.LightLabelSemanticValueAllLights))
            {
                matchingLightbulbIds = houseSpec.GetAllLightIds().ToList();
            }
            else
            {
                var matchingRoom = houseSpec.GetRoom(roomId);

                if (matchingRoom == null)
                {
                    Console.WriteLine($"Executing light action with room id {roomId}.  But no such room can be found.");
                    return;
                }

                matchingLightbulbIds = matchingRoom.LightIds.ToList();
            }

            foreach(var lightBulbId in matchingLightbulbIds)
            {
                await HueBulbClient.SetLightOnState(lightBulbId, lightOnState);
            }
        }

        private void SetNewSpeechTime(DateTime curDateTime)
        {
            lock(checkLastSpeechLock)
            {
                LastSpeechTime = curDateTime;
            }
        }

        private bool IsSpeechTimeout(DateTime curDateTime)
        {
            lock (checkLastSpeechLock)
            {
                var elapsedTicks = curDateTime.Ticks - LastSpeechTime.Ticks;
                var elapsedSpan = new TimeSpan(elapsedTicks);

                return elapsedSpan.TotalMilliseconds >= CommandSilenceTimeout;
            }
        }
    }
}
