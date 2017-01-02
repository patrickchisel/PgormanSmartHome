using HouseSpecRepo.cs;
using HueBulbRestLibrary;
using Newtonsoft.Json.Linq;
using SpeechRecognitionWebApp.ActionModel;
using SpeechRecognitionWebApp.SpeechRecognition;
using SubjectModels;
using SubjectModels.HouseModel;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Web.Http;

namespace SpeechRecognitionWebApp.Controllers
{
    public class SpeechController : ApiController
    {
        private const string HueUserName = "BYhDFTtr4dIjUTh6qAZh6yqhN5aP0R4A9CZigRwV";
        private const string HueBaseUrl = "http://192.168.1.101/";

        private HouseSpecRepository HouseSpecRepo { get; set; }

        public SpeechController()
        {
            HouseSpecRepo = new HouseSpecRepository();
        }

        //TODO pass in soundf params
        [Route("api/checkspeechinit")]
        [HttpPost]
        public async Task<IHttpActionResult> CheckSpeechForInitCommand()
        {
            var hueClient = new HueBulbClientLib(HueBaseUrl, HueUserName);

            var bytes = Task.Run(async () => await Request.Content.ReadAsByteArrayAsync()).Result;
            if (bytes.Length == 0)
            {
                return BadRequest("No sound information was provided");
            }

            var houseSpec = HouseSpecRepo.GetHouseSpec();
            LightSpeechRecognition handler = new LightSpeechRecognition();
            LightActionInfo actionInfo = handler.RunRecognizerOnSound(bytes, houseSpec);

            // TODO handle computer with a token of some sort and check that when used again it is correct.
            if (actionInfo != null && string.Equals(actionInfo.Action, LightSpeechRecognition.CommandInitAction))
            {
                return Ok(new JObject() { ["Result"] = true });
            }

            return Ok(new JObject() { ["Result"] = false });
        }

        // NOTE: If I make this controller method async it is deadlocking with speech recog somehow.
        [Route("api/speechrecognition")]
        [HttpPost]
        public async Task<IHttpActionResult> PerformSpeechRecognition()
        {
            try
            {
                var hueClient = new HueBulbClientLib(HueBaseUrl, HueUserName);

                var bytes = Task.Run(async() => await Request.Content.ReadAsByteArrayAsync()).Result;
                if (bytes.Length == 0)
                {
                    return BadRequest("No sound information was provided");
                }

                var houseSpec = HouseSpecRepo.GetHouseSpec();
                LightSpeechRecognition handler = new LightSpeechRecognition();
                LightActionInfo actionInfo = handler.RunRecognizerOnSound(bytes, houseSpec);

                // TODO I think I should separate these grammars.
                // TODO handle computer with a token of some sort and check that when used again it is correct.
                if (actionInfo == null || string.Equals(actionInfo.Action, LightSpeechRecognition.CommandInitAction))
                {
                    return Ok(new ClientCommandResults { CommandExecuted = false }.ToJson());
                }

                Task.Run(async() => await ExecuteLightAction(actionInfo, houseSpec, hueClient)).Wait();


                var results = new ClientCommandResults
                {
                    CommandExecuted = true,
                    Message = $"Command {actionInfo.Action} was executed"
                };
                return Ok(results.ToJson());
            }
            catch (Exception e)
            {
                return InternalServerError(e);
            }
            
        }

        private async Task ExecuteLightAction(LightActionInfo actionInfo, HouseSpec houseSpec, HueBulbClientLib hueClient)
        {
            bool lightOnState = actionInfo.Action.Equals(TurnOnVoiceAction.TurnOnLightsSemanticValue) ? true : false;
            var roomId = actionInfo.Identifier;

            List<string> matchingLightbulbIds;

            if (roomId.Equals(LightVoiceIdentifier.LightLabelSemanticValueAllLights))
            {
                matchingLightbulbIds = houseSpec.GetAllLightIds().ToList();
            }
            else
            {
                var matchingRoom = houseSpec.GetRoom(roomId);

                if (matchingRoom == null)
                {
                    return;
                }

                matchingLightbulbIds = matchingRoom.LightIds.ToList();
            }

            foreach (var lightBulbId in matchingLightbulbIds)
            {
                await hueClient.SetLightOnState(lightBulbId, lightOnState);
            }
        }

        [Route("api/testspeechapi")]
        [HttpGet]
        public async Task<IHttpActionResult> TestSpeechApi()
        {
            return Ok(new JObject { ["Message"] = "TEST SPEECH API"});
        }
    }
}