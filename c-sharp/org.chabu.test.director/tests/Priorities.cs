using org.chabu.test.director.prot;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace org.chabu.test.director.tests
{
    public class Priorities : ITest
    {
        public string Name { get; }
        public string Description { get; }

        public Priorities()
        {
            Name = this.GetType().Name;
            Description = @"Measure the bandwidth or multiple channels, configured with same and different priorities.";
        }


        public async Task Run(TestCtx ctx)
        {
            await Task.CompletedTask;
        }
    }
}


