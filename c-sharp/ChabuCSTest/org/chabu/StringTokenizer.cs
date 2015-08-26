using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace ChabuCSTest.org.chabu
{
    class StringTokenizer
    {
        private IEnumerable<string> words;

        public StringTokenizer(string line)
        {
            var regex = new Regex(@"\b[\s,\.-:;]*");
            words = regex.Split(line).Where(x => !string.IsNullOrEmpty(x));
        }

        internal bool hasMoreTokens()
        {
            throw new NotImplementedException();
        }

        internal string nextToken()
        {
            throw new NotImplementedException();
        }
    }
}
