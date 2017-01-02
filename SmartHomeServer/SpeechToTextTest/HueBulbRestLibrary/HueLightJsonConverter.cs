using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HueBulbRestLibrary
{
    public static class HueLightJsonConverter
    {
        public static List<HueLight> ConvertFromJsonDictionary(JObject jsonDictionary)
        {
            var lights = new List<HueLight>();

            foreach (var kvpJObj in jsonDictionary.Children())
            {
                var lightJObj = kvpJObj?.Children().OfType<JObject>().FirstOrDefault();

                if(lightJObj == null)
                {
                    continue;
                }

                lights.Add(ConvertFromJson(lightJObj));
            }

            return lights;
        }

        public static HueLight ConvertFromJson(JObject json)
        {
            var id = ParseRequiredStringProperty(json, "uniqueid");
            var name = ParseRequiredStringProperty(json, "name");
            var stateObj = json["state"] as JObject;

            var lightState = GetLightStateFromJson(stateObj);

            if(lightState == null)
            {
                throw new JsonDeserializationException("No state object could be deserialized.");
            }

            return new HueLight
            {
                Id = id,
                Name = name,
                LightState = lightState
            };
        }

        public static LightState GetLightStateFromJson(JObject json)
        {
            if(json == null)
            {
                return null;
            }

            return new LightState
            {
                On = ParseRequiredBoolProperty(json, "on")
            };
        }

        private static string ParseRequiredStringProperty(JObject jObject, string fieldName)
        {
            if(jObject == null)
            {
                throw new JsonDeserializationException($"Could not deserialize field {fieldName} from null JObject.");
            }

            var value = jObject[fieldName]?.Value<string>();

            if(string.IsNullOrWhiteSpace(value))
            {
                throw new JsonDeserializationException($"Could not deserialize hue light properties since property \"{fieldName}\" was empty.");
            }

            return value;
        }

        private static bool ParseRequiredBoolProperty(JObject jObject, string fieldName)
        {
            if (jObject == null)
            {
                throw new JsonDeserializationException($"Could not deserialize field {fieldName} from null JObject.");
            }

            var stringVal = jObject[fieldName]?.Value<string>();
            if (string.IsNullOrEmpty(stringVal))
            {
                throw new JsonDeserializationException($"Value for property \"{fieldName}\" was null or empty.");
            }

            bool val;
            var success = bool.TryParse(stringVal, out val);

            if (!success)
            {
                throw new JsonDeserializationException($"Could not deserialize property \"{fieldName}\" as it had invalid value {stringVal}");
            }

            return val;
        }

    }
}
