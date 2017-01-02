namespace SpeechToTextTest.VoiceRecognition
{
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Speech.Recognition;

    public class LightVoiceIdentifier : IVoiceIdentifier
    {
        public const string LightLabelSemanticValueAllLights = "LIGHT_IDENTIFIER_ALL";

        public IList<string> LightVoiceLabels { get; set; }

        public string LightLabelSemanticValue { get; set; }

        public LightVoiceIdentifier()
        {
        }

        public LightVoiceIdentifier(string semanticValue, IList<string> voiceLabels)
        {
            LightLabelSemanticValue = semanticValue;
            LightVoiceLabels = voiceLabels;
        }

        public static LightVoiceIdentifier GetAllLightsIdentifier()
        {
            return new LightVoiceIdentifier
            {
                LightLabelSemanticValue = LightLabelSemanticValueAllLights,
                LightVoiceLabels = LightsGrammars.LightIdentifiersAll.ToList(),
            };
        }

        public Choices ToChoices()
        {
            if(LightVoiceLabels == null || !LightVoiceLabels.Any())
            {
                throw new Exception("Could not convert light voice identifier to choices because no labels were present.");
            }

            var choices = new Choices();
            choices.Add(LightVoiceLabels.ToArray());
            return choices;
        }
    }
}
