using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using StructureMap;

namespace org.chabu.test.director
{
    public interface ITire
    {
        
    }

    public interface IRim
    {
        
    }

    public interface IWheel
    {
        string Label { get; }
    }

    public class Wheel : IWheel
    {
        public class CtorArgs
        {
            public string Label;
        }
        private readonly IRim _rim;
        private readonly ITire _tire;
        public string Label { get; private set; }
        public Wheel(IRim rim, ITire tire, CtorArgs args )
        {
            Label = args.Label;
            _rim = rim;
            _tire = tire;
        }

        public override string ToString()
        {
            return $@"[Wheel {_rim}-{_tire}-{Label}]";
        }
    }

    public interface ICar
    {
        IWheel WheelFrontLeft { get; }
        IWheel WheelFrontRight { get; }
        IWheel WheelRearLeft { get; }
        IWheel WheelRearRight { get; }

    }

    public class AlloyRim : IRim
    {
        public override string ToString()
        {
            return "[AlloyRim]";
        }
    }

    public class TireMichelin : ITire
    {
        public override string ToString()
        {
            return "[TireMichelin]";
        }
    }

    public class Porsche : ICar
    {
        public IWheel WheelFrontLeft { get; }
        public IWheel WheelFrontRight { get; }
        public IWheel WheelRearLeft { get; }
        public IWheel WheelRearRight { get; }
        public Porsche(IWheelFactory wheelFactory)
        {
            WheelFrontLeft = wheelFactory.Create("Front-Left");
            WheelFrontRight = wheelFactory.Create("Front-Right");
            WheelRearLeft = wheelFactory.Create("Rear-Left");
            WheelRearRight = wheelFactory.Create("Rear-Right");
        }

        public override string ToString()
        {
            return $@"[Porsche {WheelFrontLeft}:{WheelFrontRight}:{WheelRearLeft}:{WheelRearRight}]";
        }
    }

    public interface IWheelFactory
    {
        IWheel Create(string label);
    }

    public class WheelFactory : IWheelFactory
    {
        private readonly IContainer _container;

        public WheelFactory(IContainer container)
        {
            _container = container;
        }

        public IWheel Create(string label)
        {
            var args = new Wheel.CtorArgs {Label = label};
            return _container
                .With("args").EqualTo(args)
                .GetInstance<IWheel>();
        }
    }


    [TestClass]
    public class SmTest
    {
        [TestMethod]
        public void FactoryTest()
        {
            var c = new Container(_ => 
            {
                _.For<IWheel>().Use<Wheel>();
                _.For<ICar>().Use<Porsche>();
                _.For<ITire>().Use<TireMichelin>();
                _.For<IRim>().Use<AlloyRim>();
                _.For<IWheelFactory>().Use<WheelFactory>();
            });

            var car = c.GetInstance<ICar>();
            Assert.Equals( car.WheelFrontLeft.Label, "Front-Left" );
            //Console.WriteLine("Car: {0}", car);
        }

    }
}
