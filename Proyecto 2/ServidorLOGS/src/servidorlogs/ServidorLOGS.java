/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorlogs;

/**
 *
 * @author netbeans
 */
public class ServidorLOGS {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        Nodo n = new Nodo(Integer.parseInt(args[0]), args[1]);
        n.iniciar();
    }
    
}