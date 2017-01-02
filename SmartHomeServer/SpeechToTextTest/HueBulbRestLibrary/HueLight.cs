using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HueBulbRestLibrary
{
    public class HueLight
    {
        public string Id { get; set; }

        public string Name { get; set; }

        public LightState LightState { get; set; }
        
    }
}
