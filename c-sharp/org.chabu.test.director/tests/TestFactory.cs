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
            var tests = new List<ITest>
            {
                new BandwidthUniDir( 100000 ),
                new BandwidthUniDir( 1000000 ),
                new BandwidthUniDir( 10000000 ),
                new BandwidthUniDir( 100000000 ),
                new BandwidthUniDir( 300000000 ),
                new BandwidthUniDir( 700000000 ),
                new BandwidthUniDir( 1000000000 ),
                new BandwidthBiDir( 100000 ),
                new BandwidthBiDir( 1000000 ),
                new BandwidthBiDir( 10000000 ),
                new BandwidthBiDir( 100000000 ),
                new BandwidthBiDir( 300000000 ),
                new BandwidthBiDir( 700000000 ),
                new BandwidthBiDir( 1000000000 ),
                new Priorities(),
                new Reconnect()
            };
            return tests;
        }
    }
}
