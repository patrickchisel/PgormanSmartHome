using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace SpeechToTextTest
{
    public static class CommandConstants
    {
        public const string InitiateCommandsPhrase = "computer";
        public const string CancelProgramCommand = "cancel override";

        public const string LightIdentifierSemanticKey = "LIGHT_IDENTIFIER";
        public const string LightActionSemanticKey = "LIGHT_ACTION";
        public const string CommandSubjectSemanticKey = "ACTION_SUBJECT";
    }
}
