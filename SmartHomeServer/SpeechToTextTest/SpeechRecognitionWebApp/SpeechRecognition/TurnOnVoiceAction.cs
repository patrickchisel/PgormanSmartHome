using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Speech.Recognition;
using System.Text;
using System.Threading.Tasks;

namespace SpeechRecognitionWebApp.SpeechRecognition
{
    public class TurnOnVoiceAction : IVoiceAction
    {
        public const string TurnOnLightsSemanticValue = "TURN_ON";

        public Choices ToChoices()
        {
            var choices = new Choices();

            foreach(var commandText in LightsGrammars.TurnOnLightCommands)
            {
                choices.Add(new SemanticResultValue(commandText, TurnOnLightsSemanticValue));
            }

            return choices;
        }
    }
}
