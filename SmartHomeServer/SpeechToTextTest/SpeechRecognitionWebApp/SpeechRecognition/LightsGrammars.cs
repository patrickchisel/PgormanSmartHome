﻿using System.Collections.Generic;

namespace SpeechRecognitionWebApp.SpeechRecognition
{
    public static class LightsGrammars
    {
        public static IReadOnlyList<string> TurnOnLightCommands = new List<string> { "Turn on", "enable" , "activate" };

        public static IReadOnlyList<string> TurnOffLightCommands = new List<string> { "Turn off", "disable", "deactivate" };

        public static IReadOnlyList<string> LabelsForLightEntities = new List<string> { "lights", "light", "lamp", "lamps", "lightswitch", "lightswitches"};

        public static IReadOnlyList<string> LightIdentifiersAll = new List<string> { "all", "every" };
    }
}
