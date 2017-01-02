using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HueBulbRestLibrary
{
    public class ClientCommandResults
    {
        public bool CommandExecuted { get; set; }

        public string Message { get; set; }

        public JObject ToJson()
        {
            return new JObject
            {
                ["CommandExecuted"] = CommandExecuted,
                ["Mesasge"] = Message
            };
        }
    }
}
