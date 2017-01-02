using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace HueBulbRestLibrary
{
    public class HueBulbClientLib
    {
        public string RestBaseUrl { get; set; }

        private string UserName { get; set; }

        public HueBulbClientLib(string restBaseUrl, string userName)
        {
            RestBaseUrl = restBaseUrl;
            UserName = userName;
        }

        public async Task<HueClientResponse<bool>> SetLightOnState(string lightName, bool on)
        {
            var bodyJson = new JObject();
            bodyJson["on"] = on;

            using (var client = new HttpClient())
            {
                client.BaseAddress = new Uri(RestBaseUrl);

                try
                {
                    var response = await client.PutAsync($"/api/{UserName}/lights/{lightName}/state", new StringContent(bodyJson.ToString()));
                    var responseContent = await response.Content.ReadAsStringAsync();

                    if (!response.IsSuccessStatusCode)
                    {
                        return new HueClientResponse<bool>(response.StatusCode, response.ReasonPhrase + ": " + responseContent);
                    }

                    return new HueClientResponse<bool>(response.StatusCode, true);
                }
                catch (Exception e)
                {
                    throw new Exception(
                        "Unexpected exception occurred when trying to call SetLightOnState.  See inner exception for details.", e);
                }
            }
        }

        public async Task<HueClientResponse<List<HueLight>>> GetAllLights()
        {
            using(var client = new HttpClient())
            {
                client.BaseAddress = new Uri(RestBaseUrl);

                try
                {
                    var response = await client.GetAsync($"/api/{UserName}/lights");
                    var responseContent = await response.Content.ReadAsStringAsync();

                    if(!response.IsSuccessStatusCode)
                    {
                        return new HueClientResponse<List<HueLight>>(response.StatusCode, response.ReasonPhrase + ": " + responseContent);
                    }

                    try
                    {
                        var jsonObject = JObject.Parse(responseContent);
                        var lights = HueLightJsonConverter.ConvertFromJsonDictionary(jsonObject);
                        return new HueClientResponse<List<HueLight>>(response.StatusCode, lights);

                    } catch(Exception e)
                    {
                        throw new Exception(
                            "Server response contained malformatted Json Response.  See inner exception for details:", e);
                    }
                } catch(Exception e)
                {
                    throw new Exception(
                        "Unexpected exception occurred when trying to call GetAllLights.  See inner exception for details.", e);
                }
            }
        }
    }
}
