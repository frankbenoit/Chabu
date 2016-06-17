using System;
using System.Xml.Serialization;

namespace org.chabu.test.director.prot
{
    [Serializable]
    [XmlInclude(typeof(ParameterValue))]
    [XmlInclude(typeof(ParameterWithChilds))]
    public abstract class Parameter
    {
        public string Name { get; set; }
    }
}