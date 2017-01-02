using System;

namespace HueBulbRestLibrary
{
    public class JsonDeserializationException : Exception
    {
        public JsonDeserializationException(string message) : base(message)
        {
        }

        public JsonDeserializationException(string message, Exception innerException) : base(message, innerException)
        {
        }
    }
}
