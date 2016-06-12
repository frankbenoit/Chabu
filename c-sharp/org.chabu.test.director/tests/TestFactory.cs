using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using org.chabu.test.director.prot;

namespace org.chabu.test.director.tests
{
    public class TestFactory
    {
        public List<ITest> GetList()
        {
            var tests = new List<ITest> {new Bandwidth(), new Priorities(), new Reconnect()};
            return tests;
        }
    }
}
