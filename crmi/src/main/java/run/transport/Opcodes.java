package run.transport;

public interface Opcodes
{
  /**
   * Packet: Nessuna operazione
   */
  byte NOP = 0x00;

  /**
   * Packet: INIT
   */
  byte INIT = 0x01;

  /**
   * ACKI
   */
  byte ACKI = 0x02;

  /**
   * Packet: DISC disconnessione
   */
  byte DISC = 0x03;

  /**
   * ACKD
   */
  byte ACKD = 0x04;


  /**
   * Packet: Init di un valore di ritorno: RET
   */
  byte RET = 0x05;

  /**
   * ACKR
   */
  byte ACKR = 0x06;

  /**
   * Packet: DATA
   */
  byte DATA = 0x07;

  /**
   * Richiesta di un packet
   */
  byte RN = 0x08;



  /*
   ** Connessione in IDLE
   */
  byte IDLE = 0x10;

  /*
   * NEW
  byte NEW = 0x11;
   */

  /**
   * Distribuited Garbage Collection
   */
  byte DGC = (byte) 0x82;

  /**
   * RESET dello stream
   */
  byte RESET = (byte) 0x84;

  /**
   * Callgram: ERRORE: chiamata non eseguita
   */
  byte ERROR = -1;

}

/*
 * istanza di un oggetto, a seguire la serializzazione
  int ISTANCE = 0x05;

   * Riferimento ad un oggetto gia' serializzato, a seguire il riferimento
   int REFERENCE = 0x06;
   */

