using System;

namespace org.chabu.test.director.prot
{
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