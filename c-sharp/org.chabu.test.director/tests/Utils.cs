using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace org.chabu.test.director.tests
{
    static class Utils
    {
        public static string GetSizeAsString(long size)
        {
            if (size < 10000L)
            {
                return $@"{size} B";
            }

            if (size < 10000000L)
            {
                return $@"{size / 1e3:0.##} KB";
            }

            if (size < 10000000000L)
            {
                return $@"{size / 1e6:0.##} MB";
            }

            return $@"{size / 1e9:0.##} GB";
        }
    }
}
