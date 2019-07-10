import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class Planificador {
    private Queue<Pcb> procesosRestantes;
    private Queue<Pcb> procesosTerminados;
    private int cantidadNucleosActivos;
    private ReentrantLock lockNucleosActivos; // Sincroniza el acceso a la variable cantidadNucleosActivos

    public Planificador(){
        procesosRestantes = new LinkedList<Pcb>();
        procesosTerminados = new LinkedList<Pcb>();
        lockNucleosActivos = new ReentrantLock();
    }

    /**
     * Agrega un nuevo proceso a la cola de procesos restantes
     * @param nuevoPcb nuevoPcb el nuevo proceso a agregar en la cola
     */
    public void agregarProcesosRestantes(Pcb nuevoPcb) {
        this.procesosRestantes.add(nuevoPcb);
    }

    /**
     * Agrega un nuevo proceso a la cola de procesos terminados
     * @param nuevoPcb nuevoPcb el nuevo proceso a agregar en la cola
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
     * Nos indica si hay hilillos restantes en la lista de hilillos
     * @return true si hay algún hilillo y false sino hay hilillo
     */
    public boolean hayHilillo() {
        if(this.procesosRestantes.size() > 0)
            return true;
        else
            return false;
    }

    /**
     * Nos permite llevar un control de la cantidad de núcleos activos en el sistema agregando uno
     */
    public void agregarNucleoActivo() {
        ++this.cantidadNucleosActivos;
    }

    /**
     * Nos permite llevar un control de la cantidad de núcleos activos en el sistema descontando uno de los ya existentes
     */
    public void desactivarNucleoActivo() {
        --this.cantidadNucleosActivos;
    }

    /**
     * Permite mantener sincronizado el valor de la variable cantidadNucleosActuvos
     */
    public void ponerCandadoNucleosActivos(){
        lockNucleosActivos.lock();
    }

    /**
     * Permite mantener sincronizado el valor de la variable cantidadNucleosActuvos
     */
    public void liberarCandadoNucleosActivos(){
        lockNucleosActivos.unlock();
    }

    public int getCantidadNucleosActivos() {
        return cantidadNucleosActivos;
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
