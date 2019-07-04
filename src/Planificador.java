import java.util.LinkedList;
import java.util.Queue;

public class Planificador {
    private Queue<Pcb> procesosRestantes;
    private Queue<Pcb> procesosTerminados;

    public Planificador(){
        procesosRestantes = new LinkedList<Pcb>();
        procesosTerminados = new LinkedList<Pcb>();
    }

    /**
     * Agrega un nuevo proceso a la cola de procesos restantes
     * @param Pcb nuevoPcb el nuevo proceso a agregar en la cola
     */
    public void agregarProcesosRestantes(Pcb nuevoPcb) {
        this.procesosRestantes.add(nuevoPcb);
    }

    /**
     * Agrega un nuevo proceso a la cola de procesos terminados
     * @param Pcb nuevoPcb el nuevo proceso a agregar en la cola
     */
    public void agregarProcesosTerminados(Pcb nuevoPcb) {
        this.procesosTerminados.add(nuevoPcb);
    }

    /**
     * Retorna el valor de la cabeza de la cola y lo elimina de la misma
     */
    public Pcb usarProcesosRestantes() {
        return this.procesosRestantes.poll();
    }

    /**
     * Retorna el valor de la cabeza de la cola y lo elimina de la misma
     */
    public Pcb usarProcesosTerminados() {
        return this.procesosTerminados.poll();
    }

    public boolean existenProcesosRestantes(){
        if(this.procesosRestantes.size() == 0){
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * Imprime en pantalla cada uno de los Pcbs que están en la cola de terminados
     */
    public void print(){
        while(this.procesosTerminados.size() > 0){
            Pcb proceso = this.procesosTerminados.poll();
            proceso.print();
        }
    }
}
