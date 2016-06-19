using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Serialization;

namespace org.chabu.test.director.prot
{
    [Serializable]
    public class XferItem
    {
        public Category Category { get; set; }
        public string Name { get; set; }
        public int CallIndex { get; set; }
        public Parameter[] Parameters { get; set; }

        [XmlIgnore]
        private Dictionary<string,ParameterValue> values = new Dictionary<string, ParameterValue>();

        public string GetString(string path)
        {
            var pv = FindParameterValue(path, true);
            return pv.Value;
        }

        public int GetInt(string path, int defValue)
        {
            var pv = FindParameterValue(path, false);
            if( pv == null ) return defValue;
            return pv.ValueAsInt();
        }

        public int GetInt(string path)
        {
            var pv = FindParameterValue(path, true);
            return pv.ValueAsInt();
        }

        public long GetLong(string path)
        {
            var pv = FindParameterValue(path, true);
            return pv.ValueAsLong();
        }

        public double GetDouble(string path)
        {
            var pv = FindParameterValue(path, true);
            return pv.ValueAsDouble();
        }

        public bool IsError()
        {
            return GetInt("IsError", 0) != 0;
        }

        public string GetErrorMessage()
        {
            return GetString("Message");
        }

        private ParameterValue FindParameterValue(string path, bool throwOnNotFound)
        {
            var parts = path.Split('/');
            var par = FindParameter(Parameters, parts, 0, throwOnNotFound);
            if (par == null)
            {
                if (throwOnNotFound)
                {
                    throw new KeyNotFoundException(path);
                }
                else
                {
                    return null;
                }
            }
            var pv = (ParameterValue)par;
            return pv;
        }

        private static Parameter FindParameter(Parameter[] par, string[] pathParts, int pathIndex, bool throwOnNotFound)
        {
            var part = pathParts[pathIndex];
            //Console.WriteLine(part);
            foreach (var t in par)
            {
                if (!t.Name.Equals(part)) continue;
                var nextIndex = pathIndex + 1;
                var morePathParts = nextIndex < pathParts.Length;
                if (!morePathParts)
                {
                    return t;
                }

                var pc = t as ParameterWithChilds;
                if (pc != null)
                {
                    return FindParameter(pc.Childs, pathParts, nextIndex, throwOnNotFound);
                }

                if (throwOnNotFound)
                {
                    throw new KeyNotFoundException();
                }
                return null;
            }
            return null;
        }
    }
}
