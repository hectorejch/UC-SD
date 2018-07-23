/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author urc
 */
public class ManejadorMensaje extends Thread{
    private Proceso p;
    ObjectInputStream is;
    HashMap<String, Proceso> hmp = new HashMap<>();
    
    public ManejadorMensaje(Proceso p, ObjectInputStream is) {
        this.p = p;
        this.is = is;
    }
    
    @Override
    public void run() {
        try {
            hmp = (HashMap<String, Proceso>) this.is.readObject();
            if (hmp.containsKey("IN")) {
                Proceso p = hmp.get("IN");
                if (!this.p.getProcesos().containsKey(p.getId())) {
                    // Agrego proceso a la lista de procesos
                    this.p.agregarProceso(p);
                    if (this.p.getTipoProceso() == TipoProceso.INTRODUCTOR) {
                      // devuelvo la lista de procesos a P
                      this.enviarListaProcesos(this.p.getProcesos(), p.getIpPropia(), p.getPuertoPropio());
                    }
                    this.p.actualizarVecinos();
                    this.comunicarEstadoProcesoVecinos(p, "IN");
                }
            } else if (hmp.containsKey("EXIT")) {
                Proceso p = hmp.get("EXIT");
                if (this.p.getProcesos().containsKey(p.getId())) {
                    quitarProcesoTabla(this.p,p,"EXIT");
                    System.out.println("El proceso "+p.getId()+" salio del anillo.\n");
                }
            } else if (hmp.containsKey("DEATH")) {
                Proceso p = hmp.get("DEATH");
                quitarProcesoTabla(this.p,p,"DEATH");
            } else if (hmp.containsKey("LIVE")) {
                // Obtengo el proceso que viene en el mensaje
                Proceso p = hmp.get("LIVE");
                // renuevo el temporizador asociado a este proceso
                this.p.setTemporizadorProceso(p.getId(), System.currentTimeMillis());
            }
        } catch (IOException ex) {
            Logger.getLogger(ManejadorMensaje.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ManejadorMensaje.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                this.is.close();
            } catch (IOException ex) {
                Logger.getLogger(ManejadorMensaje.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void enviarListaProcesos(TreeMap<Integer, Proceso> tmp, InetAddress ip, int puerto){
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(outputStream);
            os.writeObject(tmp);
            byte[] data = outputStream.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip, puerto);
            ds.send(sendPacket);
            ds.close();
            os.close();
        } catch (SocketException ex) {
            Logger.getLogger(Proceso.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Proceso.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Proceso.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    public synchronized void enviarProceso(HashMap<String, Proceso> hmp, InetAddress ip, int puerto){
        DatagramSocket ds = null;
        ObjectOutputStream os = null;
        try {
            ds = new DatagramSocket();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            os = new ObjectOutputStream(outputStream);
            os.writeObject(hmp);
            byte[] data = outputStream.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, ip, puerto);
            ds.send(sendPacket);
        } catch (SocketException ex) {
            Logger.getLogger(Proceso.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Proceso.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Proceso.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                ds.close();
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(MarcaPaso.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Envia a los vecinos un mensaje con el Proceso y la accion a ser realizado sobre el mismo
     * @param p proceso que se envia a los vecinos
     * @param e indica que tipo de mensaje será enviado a los vecinos (IN, EXIT, DEATH, LIVE)
     */
    public void comunicarEstadoProcesoVecinos(Proceso p, String e){
        HashMap<String, Proceso> mensaje = new HashMap<>();
        Iterator<Entry<Integer, Proceso>> it = this.p.getVecinos().entrySet().iterator();
        // Armar mensaje a ser enviado
        mensaje.put(e, p);
        while (it.hasNext()) {
            Proceso vecino = it.next().getValue();
            synchronized (mensaje) {
                    this.enviarProceso(mensaje, vecino.getIpPropia(), vecino.getPuertoPropio());
            }
        }
    }
    
    /**
     * Quita el un proceso de la tabla de procesos, tabla de vecinos, de otro proceso
     * @param tablaProceso proceso que posee la tabla de procesos, tabla de vecinos, de la cual el proceso se debe remover.
     * @param ProcAQuitar proceso que se debe quitar de la tabla de procesos y la tabla de vecinos.
     * @param motivo motivo por el cual se quita el proceso de las tablas
     */
    private void quitarProcesoTabla(Proceso tablaProceso, Proceso ProcAQuitar, String motivo){
        // Lo quito de la tabla de procesos
        tablaProceso.quitarProceso(ProcAQuitar);
        // Lo quito de la tabla de vecinos
        // tablaProceso.quitarVecino(ProcAQuitar);
        // Actualizo la tabla de vecinos
        tablaProceso.actualizarVecinos();
        // Comunico a los vecinos restantes que se ha muerto un proceso
        comunicarEstadoProcesoVecinos(ProcAQuitar, motivo);
    }
}
