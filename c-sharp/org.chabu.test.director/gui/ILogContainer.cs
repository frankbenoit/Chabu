using System;

namespace org.chabu.test.director.gui
{
    public delegate void ChangedEventHandler(object sender, EventArgs e);
    public interface ILogContainer
    {
        void Add(string fmt, params object[] args);
        void Clear();
        int Length { get; }
        string GetText();
        event ChangedEventHandler Updated;
    }
}