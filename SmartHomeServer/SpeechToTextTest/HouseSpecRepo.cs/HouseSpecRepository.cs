using SubjectModels.HouseModel;
using System.Collections.Generic;

namespace HouseSpecRepo.cs
{
    public class HouseSpecRepository
    {
        public HouseSpec GetHouseSpec()
        {
            // TODO there is no reason I cannot detect this from hue, except for the voice part.  FK for the voice part.
            return new HouseSpec(new List<RoomSpec>
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
        }
    }
}
