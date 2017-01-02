using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Speech.Recognition;
using System.Text;
using System.Threading.Tasks;

namespace SpeechRecognitionWebApp.SpeechRecognition
{
    public interface IGrammarEntity
    {
        Choices ToChoices();
    }
}
