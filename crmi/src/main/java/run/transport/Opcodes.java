/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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

