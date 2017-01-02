using System;
using System.Collections.Generic;

namespace SpeechToTextTest.HouseModel
{
    public class HouseSpec
    {
        private Dictionary<string, RoomSpec> RoomIdsToRoomInternal { get;set;}

        public IReadOnlyDictionary<string, RoomSpec> RoomIdsToRoom {
            get
            {
                return RoomIdsToRoomInternal;
            }
        }

        private HashSet<string> LightIds { get; set; }


        public HouseSpec(List<RoomSpec> rooms)
        {
            RoomIdsToRoomInternal = new Dictionary<string, RoomSpec>();
            LightIds = new HashSet<string>();
            rooms.ForEach(r => AddRoom(r));
        }

        public HouseSpec() : this(new List<RoomSpec>())
        {
        }

        public void AddRoom(RoomSpec room)
        {
            if(RoomIdsToRoom.ContainsKey(room.Id))
            {
                throw new ArgumentException($"Room with id {room.Id} has already been added.");
            }

            RoomIdsToRoomInternal[room.Id] = room;
            
            foreach(var lightId in room.LightIds)
            {
                LightIds.Add(lightId);
            }
        }

        public RoomSpec GetRoom(string id)
        {
            if(!RoomIdsToRoomInternal.ContainsKey(id))
            {
                return null;
            }

            return RoomIdsToRoomInternal[id];
        }

        public HashSet<string> GetAllLightIds()
        {
            var results = new HashSet<string>();

            foreach(var kvp in RoomIdsToRoomInternal)
            {
                var lightIds = kvp.Value.LightIds;
                foreach(var lightId in lightIds)
                {
                    results.Add(lightId);
                }
            }

            return results;
        }
    }
}
