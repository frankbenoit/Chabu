using System;
using System.Globalization;

namespace org.chabu.test.director.prot
{
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
}