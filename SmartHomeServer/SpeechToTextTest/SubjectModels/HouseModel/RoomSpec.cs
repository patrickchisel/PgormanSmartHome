using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace SubjectModels.HouseModel
{
    public class RoomSpec
    {
        public string Id { get; set; }

        public IReadOnlyList<string> LightIds { get; set; }

        public IReadOnlyList<string> VoiceIdentifiers { get; set; }

        public RoomSpec(string id, IReadOnlyList<string> voiceIdentifiers, IReadOnlyList<string> lightIds)
        {
            Id = id;
            VoiceIdentifiers = voiceIdentifiers;
            LightIds = lightIds;
        }
    }
}
