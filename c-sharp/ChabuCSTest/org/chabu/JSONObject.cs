using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace org.chabu
{
    class JSONObject
    {
        Dictionary<string, string> data = new Dictionary<string, string>();

        public JSONObject( string jsonText )
        {
        }

        public JSONObject()
        {
        }

        public string getString(string key)
        {
            return data[key];
        }

        public int getInt(string key)
        {
            return Convert.ToInt32(data[key]);
        }


        internal JSONArray getJSONArray(string p)
        {
            throw new NotImplementedException();
        }

        internal void put(string p, int more)
        {
            throw new NotImplementedException();
        }

        internal int optInt(string p)
        {
            throw new NotImplementedException();
        }
    }
}
