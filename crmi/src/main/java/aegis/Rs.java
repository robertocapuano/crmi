package aegis;

import run.Services;
import run.RemoteException;

public interface Rs extends Services {

  int f1( X x, int[] ia, int i ) throws RemoteException;
  int[] f2( X x, X[] xa, int i ) throws RemoteException;
  X f3( int i ) throws RemoteException;
  Z f4( int n ) throws RemoteException;
  void f5( ) throws RemoteException;
  float f6( X x, long l, String s ) throws RemoteException;
  String f7( X x, int l, String s ) throws RemoteException;
  X f8( long l, String s, X x ) throws RemoteException;

}
