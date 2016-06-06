using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Serialization;

namespace org.chabu.test.director.prot
{
    public enum Category
    {
        // ReSharper disable once InconsistentNaming
        REQ,
        // ReSharper disable once InconsistentNaming
        RES,
        // ReSharper disable once InconsistentNaming
        EVT
    }

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
            var pv = FindParameterValue(path);
            return pv.Value;
        }

        public int GetInt(string path)
        {
            var pv = FindParameterValue(path);
            return pv.ValueAsInt();
        }

        public double GetDouble(string path)
        {
            var pv = FindParameterValue(path);
            return pv.ValueAsDouble();
        }

        private ParameterValue FindParameterValue(string path)
        {
            var parts = path.Split('/');
            var par = FindParameter(Parameters, parts, 0);
            if (par == null) throw new KeyNotFoundException(path);
            var pv = (ParameterValue)par;
            return pv;
        }

        private static Parameter FindParameter(Parameter[] par, string[] pathParts, int pathIndex)
        {
            var part = pathParts[pathIndex];
            Console.WriteLine(part);
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
                if ( pc == null)
                {
                    throw new KeyNotFoundException();
                }
                return FindParameter(pc.Childs, pathParts, nextIndex);
            }
            return null;
        }
    }

    [Serializable]
    [XmlInclude(typeof(ParameterValue))]
    [XmlInclude(typeof(ParameterWithChilds))]
    public abstract class Parameter
    {
        public string Name { get; set; }
    }

    [Serializable]
    public class ParameterValue : Parameter
    {
        public string Value { get; set; }

        public ParameterValue()
        {
        }
        public ParameterValue(string name, string value)
        {
            Name = name;
            Value = value;
        }
        public ParameterValue(string name, long value)
        {
            Name = name;
            Value = Convert.ToString(value);
        }
        public ParameterValue(string name, double value)
        {
            Name = name;
            Value = Convert.ToString(value, CultureInfo.InvariantCulture);
        }

        public int ValueAsInt()
        {
            return Convert.ToInt32(Value);
        }
        public double ValueAsDouble()
        {
            return Convert.ToDouble(Value);
        }
    }

    [Serializable]
    public class ParameterWithChilds : Parameter
    {
        public Parameter[] Childs { get; set; }

        public ParameterWithChilds()
        {

        }
        public ParameterWithChilds(string name, Parameter[] childs)
        {
            Name = name;
            Childs = childs;
        }
    }
}
