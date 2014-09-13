package run.session;


import run.reference.*;
import run.serialization.Container;
import run.transport.*;

/**
 * Callgram rappresenta un pacchetto di informazioni inviati/da inviare a remoto
 * Contiene un frame.
 */

public class Callgram
{

  // Valori per operation

  /**
   * Nessuna operazione
   */
  public static final byte NOP = 0x00;

  /**
   * Callgram: Chiamata di un metodo
   */
  public static final byte CALL = 0x01;

  /**
   * Callgram: Valore di ritorno di un metodo
   */
  public static final byte RETURN = 0x02;

  /**
   * Callgram: REMOTE_EXCEPTION
   */
  public static final byte REMOTE_EXCEPTION = 0x03;

  /**
   * Distribuited Garbage Collection
   */
  public static final byte DGC = (byte) 0x11;

  /**
   * Callgram: ERRORE: chiamata non eseguita
   */
  public static final byte ERROR = -1;


  /**
   * Operazione che trasmette il container. Viene trasmessa.
   */
  protected byte operation;

  /**
   * Valore di ritorno. Viene trasmessa.
   */
  protected int return_value;

  /**
   * tipo di CPU del sender. Viene trasmessa.
   */
  protected boolean little_endian;

  /**
   * Riferimento al servizio richiesto.
   * Viene trasmesso.
   */
  private RemoteServiceReference service_reference;

  /**
   * Dati locali: container.
   * Se container==null indica una operazione di servizio
   */
  protected Container container;

  /**
   * Dati locali: Sessione che ha trasmesso il callgram
   */
  protected Session session;

  /**
   * Dati locali: Listener del callgram
   */
  protected CallgramListener listener;

  /**
   * tipo di CPU del frame: little-endian/big-endian
   * Renderlo indipendente dalla piattaforma
   * intel = little-endian
   * java = big-endian
   * Stub.
   */
  public static final boolean isLittleEndian() { return true; }

  /**
   * Costruttore usato per inviare la chiamata remota
   */
  public Callgram( byte _operation, RemoteServiceReference _service_reference, CallgramListener _listener, Container _container )
  {
    operation = _operation;
    return_value = 0;
    service_reference = _service_reference;
    container = _container;
    listener = _listener;
    little_endian = isLittleEndian();
  }

  /**
   * Costruttore usato per ricostruire il callgram sullo host remoto
   */
  public Callgram( byte _operation, int _return_value, RemoteServiceReference _service_reference,  Session _session, CallgramListener _listener, Container _container )
  {
    operation = _operation;
    return_value = _return_value;
    service_reference = _service_reference;
    session = _session;
    container = _container;
    listener = _listener;
    little_endian = isLittleEndian();
  }
  /**
   * Costruttore usato per inviare la risposta ad un oggetto chiamante
   */
  public Callgram( byte _operation, int _return_value, Session _session, CallgramListener _listener, Container _container )
  {
    operation = _operation;
    return_value = _return_value;
    service_reference = null;
    session = _session;
    container = _container;
    listener = _listener;
    little_endian = isLittleEndian();
  }

  public final void freeSession()
  {
    if (session!=null)
    {
      SessionTable.free( session );
      session = null;
    }
  }

  public void free()
  {
    freeSession();

    if (container!=null)
    {
      container.free();
      container = null;
    }

    listener = null;
    service_reference = null;
  }

  public String toString()
  {
    String res ="<" + getClass().getName() + ">\n";

    res += "operation: " + operation + "\n";
    res += "service_reference" + service_reference + "\n";
    res += "return_value: " + return_value + "\n";
    res += "session: " + session + "\n";
    res += "byte_frame: " + container + "\n";

    return res;
  }

  public boolean equals( Object o )
  {
    if (this==o) return true;
    if (o==null || !( o instanceof Callgram) )
      return false;
    Callgram c = (Callgram) o;

    boolean res = true;

    res &= operation == c.operation;
    res &= return_value == c.return_value;
//    res &= service_reference.equals( c.service_reference );
    res &= java.util.Arrays.equals( container.getByteFrame(), c.container.getByteFrame() );

    return res;
  }

  public final byte getOperation()
  {
    return operation;
  }

  /**
   * Restituisce il reference al servizio remoto
   */
  public final NetAddress getRemoteAddress()
  {
    return service_reference.getAddress();
  }

  /**
   * Restituisce il reference al servizio remoto
   */
  public final RemoteServiceReference getRemoteServiceReference()
  {
    return service_reference;
  }

  public final void setRemoteServiceReference( RemoteServiceReference _remote_ref )
  {
    service_reference = _remote_ref;
  }

  public final void setOperation( byte _operation )
  {
    operation = _operation;
  }

  public final void setListener( CallgramListener _listener )
  {
    listener = _listener;
  }

  public final CallgramListener getListener( )
  {
    return listener;
  }

  public final Container getContainer()
  {
    return container;
  }

  public final Session getSession()
  {
    return session;
  }

  public final void setSession( Session _session )
  {
    session = _session;
  }

  public final int getReturnValue()
  {
    return return_value;
  }
}


/*
 * Vecchi informazioni.....
 * Formato serializzato:
 * (byte)Opcode (byte)LITTLE_ENDIAN RemoteServiceReference  CallerReference Container
 * RemoteServiceReference: ( (UTF8)service_name (UTF8)method_name (byte)NetAddressType NetAddress) )
 * CallerReference: (byte)NetAddressType NetAddress
 * Container: Frame
 * Frame: (int)length byte[length]
*/