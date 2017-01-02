using SpeechRecognitionWebApp.ActionModel;
using SubjectModels.HouseModel;
using System;
using System.IO;

using Microsoft.Speech.Recognition;
using Microsoft.Speech.AudioFormat;

namespace SpeechRecognitionWebApp.SpeechRecognition
{
    public class LightSpeechRecognition
    {
        HouseLightsCommandGrammarBuilderFactory GrammarBuilderFactory { get; set; }

        public const string CommandInitAction = "COMMAND_INIT";

        public LightSpeechRecognition()
        {
            GrammarBuilderFactory = new HouseLightsCommandGrammarBuilderFactory();
        }

        public LightActionInfo RunRecognizerOnSound(byte[] soundBytes, HouseSpec houseSpec)
        {
            var grammarBuilder = GrammarBuilderFactory.CreateGrammarBuilder(houseSpec);

            using (SpeechRecognitionEngine recognizer = new SpeechRecognitionEngine(System.Globalization.CultureInfo.CurrentCulture))
            {
                var grammar = new Grammar(grammarBuilder);

                // only way this works is if I never call any method on the recognizer
                recognizer.LoadGrammar(grammar);

                RecognitionResult result;
                using (var memStream = new MemoryStream(soundBytes))
                {
                    recognizer.SetInputToAudioStream(memStream, new SpeechAudioFormatInfo(44100, AudioBitsPerSample.Sixteen, AudioChannel.Mono));
                    //recognizer.SetInputToWaveStream(memStream);
                    result = recognizer.Recognize();
                }

                if(result == null)
                {
                    return null;
                }

                return GetActionInfoFromRecognitionResult(result);
            }
        }

        private void Recognizer_RecognizeCompleted(object sender, RecognizeCompletedEventArgs e)
        {
            int x = 5;
        }

        private void Recognizer_SpeechDetected(object sender, SpeechDetectedEventArgs e)
        {
            int x = 5;
        }

        private void Recognizer_SpeechRecognized(object sender, SpeechRecognizedEventArgs e)
        {
            int x = 5;
        }

        private LightActionInfo GetActionInfoFromRecognitionResult(RecognitionResult recogResult)
        {
            if (string.Equals(recogResult.Text, CommandConstants.InitiateCommandsPhrase, StringComparison.OrdinalIgnoreCase))
            {
                return new LightActionInfo(CommandInitAction);
            }

            else
            {
                var semantics = recogResult.Semantics;

                if (!semantics.ContainsKey(CommandConstants.CommandSubjectSemanticKey))
                {
                    throw new Exception("Grammar recognized but no subject was found on which to take an action.");
                }

                var subject = semantics[CommandConstants.CommandSubjectSemanticKey];

                if (Equals(subject.Value, LightVoiceSubject.LightSubjectSemanticValue))
                {
                    if (!semantics.ContainsKey(CommandConstants.LightIdentifierSemanticKey) || !semantics.ContainsKey(CommandConstants.LightActionSemanticKey))
                    {
                        throw new Exception("A command with the light subject must contain a light identifier and action semantic.");
                    }

                    var identifier = semantics[CommandConstants.LightIdentifierSemanticKey];
                    var action = semantics[CommandConstants.LightActionSemanticKey];
                    return new LightActionInfo(subject.Value.ToString(), action.Value.ToString(), identifier.Value.ToString());
                }
                else
                {
                    throw new Exception(string.Format("Subject value {0} was found but can not be bound to an action", subject.Value));
                }
            }
        }
    }
}