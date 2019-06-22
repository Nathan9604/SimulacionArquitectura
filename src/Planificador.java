import java.util.LinkedList;
import java.util.Queue;

public class Planificador {
    private Queue<Pcb> procesosRestantes;
    private Queue<Pcb> procesosTerminados;

    public Planificador(){
        procesosRestantes = new LinkedList<Pcb>();
        procesosTerminados = new LinkedList<Pcb>();
    }

    public void agregarProcesosRestantes(Pcb nuevoPcb) {
        this.procesosRestantes.add(nuevoPcb);
    }

    public void agregarProcesosTerminados(Pcb nuevoPcb) {
        this.procesosTerminados.add(nuevoPcb);
    }

    public Pcb usarProcesosRestantes() {
        return this.procesosRestantes.poll();
    }

    public Pcb usarProcesosTerminados() {
        return this.procesosTerminados.poll();
    }

    public void print(){
        while(this.procesosTerminados.size() > 0){
            Pcb proceso = this.procesosTerminados.poll();
            proceso.print();
        }
    }
}
