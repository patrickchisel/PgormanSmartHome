using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Speech.Recognition;
using System.Text;
using System.Threading.Tasks;

namespace SpeechRecognitionWebApp.SpeechRecognition
{
    public class TurnOffVoiceAction : IVoiceAction
    {
        public const string TurnOffLightsSemanticValue = "TURN_OFF";

        public Choices ToChoices()
        {
            var choices = new Choices();

            foreach (var commandText in LightsGrammars.TurnOffLightCommands)
            {
                choices.Add(new SemanticResultValue(commandText, TurnOffLightsSemanticValue));
            }

            return choices;
        }
    }
}
