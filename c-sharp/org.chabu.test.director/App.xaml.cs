using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using org.chabu.test.director.gui;
using org.chabu.test.director.prot;
using org.chabu.test.director.tests;
using StructureMap;

namespace org.chabu.test.director
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        private void OnStartup(object sender, StartupEventArgs e)
        {
            var di = new Container(_ =>
            {
                _.For<MainWindow>().Use<MainWindow>();
                _.For<ILogContainer>().Use<LogContainer>();
            });
            var window = di.GetInstance<MainWindow>();
            window.Show();
//            StartupUri = @"gui\MainWindow.xaml";
        }
    }


}
