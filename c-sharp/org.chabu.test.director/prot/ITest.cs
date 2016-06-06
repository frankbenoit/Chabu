using System.Threading.Tasks;
using org.chabu.test.director.tests;

namespace org.chabu.test.director.prot
{
    public interface ITest
    {
        string Name { get; }
        string Description { get; }
        Task Run(TestCtx ctx);
    }
}