using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace SpeechRecognitionWebApp.ActionModel
{
    public class LightActionInfo
    {
        public string Subject { get; set; }

        public string Action { get; set; }

        public string Identifier { get; set; }

        public LightActionInfo(string action)
        {
            Action = action;
        }

        public LightActionInfo(string subject, string action, string identifier)
        {
            Subject = subject;
            Action = action;
            Identifier = identifier;
        }

        public JObject ToFrontEndJson()
        {
            return new JObject
            {
                ["Subject"] = Subject,
                ["Action"] = Action,
                ["Identifier"] = Identifier
            };
        }
    }
}
