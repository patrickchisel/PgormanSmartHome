using SubjectModels.HouseModel;
using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Speech.Recognition;
using System.Text;
using System.Threading.Tasks;

namespace SpeechRecognitionWebApp.SpeechRecognition
{
    public class HouseLightsCommandGrammarBuilderFactory
    {
        public HouseLightsCommandGrammarBuilderFactory()
        {
        }

        // TODO decorator pattern to keep adding grammars?
        public GrammarBuilder CreateGrammarBuilder(HouseSpec houseSpec)
        {
            var initiateCommandGrammerBuilder = new GrammarBuilder(CommandConstants.InitiateCommandsPhrase);
            var lightGrammerBuilder = CreateLightCommandGrammar(houseSpec);
            var finalGrammarBuilder = CombineGrammarBuilders(initiateCommandGrammerBuilder, lightGrammerBuilder);
            return finalGrammarBuilder;
        }

        private static GrammarBuilder CombineGrammarBuilders(params GrammarBuilder[] grammars)
        {
            if (grammars.Length < 1)
            {
                throw new ArgumentException("Must pass in at least one grammar to combine.");
            }

            var choices = new Choices(grammars);
            return new GrammarBuilder(choices);
        }

        private static List<LightVoiceIdentifier> GetLightVoiceIdentifiersFromHouseSpec(HouseSpec houseSpec)
        {
            var houseLightingModel = new List<LightVoiceIdentifier>();
            houseLightingModel.AddRange(houseSpec.RoomIdsToRoom.Values.Select(r => ConvertToLightVoiceIdentifier(r)));
            houseLightingModel.Add(LightVoiceIdentifier.GetAllLightsIdentifier());
            return houseLightingModel;
        }

        private static LightVoiceIdentifier ConvertToLightVoiceIdentifier(RoomSpec room)
        {
            return new LightVoiceIdentifier(room.Id, room.VoiceIdentifiers.ToList());
        }

        private static GrammarBuilder CreateLightCommandGrammar(HouseSpec houseSpec)
        {
            var houseLightingModel = GetLightVoiceIdentifiersFromHouseSpec(houseSpec);
            var lightSubject = new LightVoiceSubject();
            var turnOnAction = new TurnOnVoiceAction();
            var turnOffAction = new TurnOffVoiceAction();

            var allLightsIdentifierChoices = ConvertIdentyListToGrammarBuilder(houseLightingModel);

            var turnOnActionChoices = turnOnAction.ToChoices();
            var turnOffActionChoices = turnOffAction.ToChoices();
            var lightsLables = lightSubject.ToChoices();

            // Action 1: Turn on lights, all
            var turnOnAllLightsGrammar = new GrammarBuilder();
            turnOnAllLightsGrammar.AppendWildcard();
            turnOnAllLightsGrammar.Append(new SemanticResultKey(CommandConstants.LightActionSemanticKey, turnOnActionChoices));
            turnOnAllLightsGrammar.AppendWildcard();
            turnOnAllLightsGrammar.Append(new SemanticResultKey(CommandConstants.LightIdentifierSemanticKey, allLightsIdentifierChoices));
            turnOnAllLightsGrammar.AppendWildcard();
            turnOnAllLightsGrammar.Append(new SemanticResultKey(CommandConstants.CommandSubjectSemanticKey, lightsLables));

            var turnOnAllLightsGrammar2 = new GrammarBuilder();
            turnOnAllLightsGrammar2.AppendWildcard();
            turnOnAllLightsGrammar2.Append(new SemanticResultKey(CommandConstants.LightActionSemanticKey, turnOnActionChoices));
            turnOnAllLightsGrammar2.AppendWildcard();
            turnOnAllLightsGrammar2.Append(new SemanticResultKey(CommandConstants.CommandSubjectSemanticKey, lightsLables));
            turnOnAllLightsGrammar2.AppendWildcard();
            turnOnAllLightsGrammar2.Append(new SemanticResultKey(CommandConstants.LightIdentifierSemanticKey, allLightsIdentifierChoices));

            // Action 2: Turn off lights, all
            var turnOffAllLightsGrammer = new GrammarBuilder();
            turnOffAllLightsGrammer.AppendWildcard();
            turnOffAllLightsGrammer.Append(new SemanticResultKey(CommandConstants.LightActionSemanticKey, turnOffActionChoices));
            turnOffAllLightsGrammer.AppendWildcard();
            turnOffAllLightsGrammer.Append(new SemanticResultKey(CommandConstants.LightIdentifierSemanticKey, allLightsIdentifierChoices));
            turnOffAllLightsGrammer.AppendWildcard();
            turnOffAllLightsGrammer.Append(new SemanticResultKey(CommandConstants.CommandSubjectSemanticKey, lightsLables));

            var turnOffAllLightsGrammer2 = new GrammarBuilder();
            turnOffAllLightsGrammer2.AppendWildcard();
            turnOffAllLightsGrammer2.Append(new SemanticResultKey(CommandConstants.LightActionSemanticKey, turnOffActionChoices));
            turnOffAllLightsGrammer2.AppendWildcard();
            turnOffAllLightsGrammer2.Append(new SemanticResultKey(CommandConstants.CommandSubjectSemanticKey, lightsLables));
            turnOffAllLightsGrammer2.AppendWildcard();
            turnOffAllLightsGrammer2.Append(new SemanticResultKey(CommandConstants.LightIdentifierSemanticKey, allLightsIdentifierChoices));

            var allLightsChoices = new Choices();
            allLightsChoices.Add(
                turnOnAllLightsGrammar,
                turnOnAllLightsGrammar2,
                turnOffAllLightsGrammer,
                turnOffAllLightsGrammer2);
            var finalLightsGrammerBuilder = new GrammarBuilder();
            finalLightsGrammerBuilder.Append(allLightsChoices);

            return finalLightsGrammerBuilder;
        }

        private static GrammarBuilder ConvertIdentyListToGrammarBuilder(List<LightVoiceIdentifier> identifiers)
        {
            List<GrammarBuilder> grammarBuilders = new List<GrammarBuilder>();
            foreach (var identifier in identifiers)
            {
                var labels = identifier.LightVoiceLabels;
                var choices = new Choices();
                foreach (var label in labels)
                {
                    choices.Add(new SemanticResultValue(label, identifier.LightLabelSemanticValue));
                }

                grammarBuilders.Add(new GrammarBuilder(choices));
            }

            return new GrammarBuilder(new Choices(grammarBuilders.ToArray()));
        }

    }
}
