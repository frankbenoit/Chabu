using System;
using System.Collections.Generic;
using System.Data;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace org.chabu.test.director.gui
{
   

    public class LogContainer : ILogContainer
    {
        private readonly List<string> _content = new List<string>(100);

        public void Add(string fmt, params object[] args)
        {
            var entryText = string.Format(fmt, args);
            _content.Add(entryText);
            Console.WriteLine(entryText);
            Updated?.Invoke(this, EventArgs.Empty);
        }

        public void Clear()
        {
            _content.Clear();
            Updated?.Invoke(this, EventArgs.Empty);
        }

        public int Length => _content.Count;

        public event ChangedEventHandler Updated;
        public string GetText()
        {
            var sb = new StringBuilder();
            foreach (var str in _content)
            {
                if( sb.Length > 0 )
                    sb.Append(Environment.NewLine);

                sb.Append(str);
            }
            return sb.ToString();
        }
    }
}
