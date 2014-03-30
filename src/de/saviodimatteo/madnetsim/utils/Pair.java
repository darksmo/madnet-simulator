package de.saviodimatteo.madnetsim.utils;

public class Pair<T, S>
{
  public Pair(T f, S s)
  { 
    fst = f;
    snd = s;   
  }

  public T getFirst()
  {
    return fst;
  }

  public S getSecond() 
  {
    return snd;
  }

  public String toString()
  { 
    return "(" + fst.toString() + ", " + snd.toString() + ")"; 
  }

  public T fst;
  public S snd;
}