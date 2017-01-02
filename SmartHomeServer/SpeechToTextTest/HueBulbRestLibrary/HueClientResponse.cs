using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace HueBulbRestLibrary
{
    public class HueClientResponse<T>
    {
        public T Result { get; set; }

        public HttpStatusCode StatusCode { get; set; }

        public string ErrorMessage { get; set; }

        public HueClientResponse(HttpStatusCode statusCode, string errorMessage)
        {
            StatusCode = statusCode;
            ErrorMessage = errorMessage;
        }

        public HueClientResponse(HttpStatusCode statusCode, T result) : this(statusCode, null)
        {
            Result = result;
        }
    }
}
