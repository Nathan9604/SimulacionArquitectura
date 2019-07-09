import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

public class Nucleo extends Thread {
    private Pcb pcb;
    private int registro[];
    private int PC;
    private int RL;
    private int IR;
    private char estadoHililloActual;
    private String idHililloActual;
    private int quantumHililloActual;

    private Planificador planificador;
    private CacheDatos cacheDatosLocal;
    private CacheDatos otroCacheDatos;
    private CacheInstrucciones cacheInstrucciones;

    private int reloj;
    private int quantumTotal;
    private int instruccionActual[];
    private int cantidadNucleosActivos;

    private CyclicBarrier barrera;

    private int idNucleo;

    private int cantidadCiclosHilillo;


    public Nucleo(CacheInstrucciones instrucciones, CacheDatos local, CacheDatos otroCacheDatos, int quantum, Planificador planificador, CyclicBarrier barrera,
                  int cantidadNucleosActivos, int idNucleo){
        this.cacheDatosLocal = local;
        this.otroCacheDatos = otroCacheDatos;
        this.cacheInstrucciones = instrucciones;
        this.registro = new int[32];
        this.instruccionActual = new int[4];
        this.planificador = planificador;
        this.reloj = 0;
        this.quantumTotal = quantum;
        this.barrera = barrera;
        this.IR = 0;
        this.cantidadNucleosActivos = cantidadNucleosActivos;
        this.idNucleo = idNucleo;
        this.cantidadCiclosHilillo = 0;
    }

    private void decodificador(int[] instruccion){
        int datos[];
        int numCiclos = 1;

        switch(instruccion[0]){
            case 19: // addi
                registro[instruccion[1]] = Alu(1, registro[instruccion[2]], instruccion[3]);
                break;

            case 71: // add
                registro[instruccion[1]] = Alu(1, registro[instruccion[2]], registro[instruccion[3]]);
                break;

            case 83: // sub
                registro[instruccion[1]] = Alu(2, registro[instruccion[2]], registro[instruccion[3]]);
                break;

            case 72: // mul
                registro[instruccion[1]] = Alu(3, registro[instruccion[2]], registro[instruccion[3]]);
                break;

            case 56: // div
                registro[instruccion[1]] = Alu(4, registro[instruccion[2]], registro[instruccion[3]]);
                break;

            case 5: // lw
                datos = new int[2];
                cacheDatosLocal.leerDato(registro[instruccion[2]] + instruccion[3], datos);
                registro[instruccion[1]] = datos[0];
                numCiclos = datos[1];
                break;

            case 37: // sw
                numCiclos = cacheDatosLocal.escribirDato(registro[instruccion[1]] + instruccion[3], registro[instruccion[2]]);
                break;

            case 99: // beq
                if(registro[instruccion[1]] == registro[instruccion[2]])
                    PC += (4 * instruccion[3]) - 4;
                break;

            case 100: //bne
                if(registro[instruccion[1]] != registro[instruccion[2]])
                    PC += (4 * instruccion[3]) - 4;
                break;

            case 51: // lr
                datos = new int[2];
                cacheDatosLocal.leerDato(registro[instruccion[2]], datos);
                registro[instruccion[1]] = 0; // todo ver si se pone 0 o el dato de esa posición datos[0];
                numCiclos = datos[1];
                cacheDatosLocal.setRl(registro[instruccion[2]]);
                break;

            case 52: // sc
                if(cacheDatosLocal.getRl() == registro[instruccion[1]])
                    numCiclos = cacheDatosLocal.escribirDato(registro[instruccion[1]], registro[instruccion[2]]);
                else
                    registro[instruccion[2]] = 0;
                break;

            case 111: // jal
                registro[instruccion[1]] = PC;
                PC = PC + instruccion[3];
                break;

            case 103: // jalr
                registro[instruccion[1]] = PC;
                PC = registro[instruccion[2]] + instruccion[3];
                break;

            case 999: // El hilillo acabo
                estadoHililloActual = 'F';
                break;
        }

        cicloReloj(numCiclos);

        this.cantidadCiclosHilillo += numCiclos; // Se le suma cantidad de ciclos de reloj que lleva el hilillo
    }

    public void run(){
        cantidadNucleosActivos++;

        while (planificador.hayHilillo()){
            cargarContexto();

            while (estadoHililloActual == 'R' && this.quantumHililloActual > 0) {
                obtenerSiguienteInstruccion();
                IR = PC;
                PC += 4;
                decodificador(instruccionActual);
                --this.quantumHililloActual;
            }

            guardarContexto();
        }
        cantidadNucleosActivos--;
    }

    private void guardarContexto() {
        this.pcb.setPc(this.PC);
        this.pcb.setIr(this.IR);
        this.pcb.setEstado(this.estadoHililloActual);
        this.pcb.setRegistro(registro);
        int ciclos = this.pcb.getCiclosReloj();
        ciclos += this.cantidadCiclosHilillo;
        this.pcb.setCiclosReloj(ciclos);

        if(this.estadoHililloActual == 'R')
            planificador.agregarProcesosRestantes(this.pcb);
        else
            planificador.agregarProcesosTerminados(this.pcb);
    }

    private void cargarContexto() {
        this.pcb = planificador.usarProcesosRestantes();
        this.cantidadCiclosHilillo = 0;

        this.idHililloActual = pcb.getId();
        this.PC = pcb.getPc();
        this.IR = pcb.getIr();
        this.estadoHililloActual = pcb.getEstado();
        this.registro = pcb.getRegistro();

        this.RL = -1;
        this.quantumHililloActual = quantumTotal;
    }

    private void obtenerSiguienteInstruccion(){
        int numCiclos = cacheInstrucciones.leerInstruccion(PC, instruccionActual);

        cicloReloj(numCiclos);
    }

    private int Alu(int operacion, int operando1, int operando2){
        int resultado = -1;

        switch(operacion){
            case 1:
                resultado = operando1 + operando2;
                break;
            case 2:
                resultado = operando1 - operando2;
                break;
            case 3:
                resultado = operando1 * operando2;
                break;
            case 4:
                resultado = operando1 / operando2;
                break;
        }
        return resultado;
    }

    public void cicloReloj(int numCiclos){
        for(int i = 0; i < numCiclos; i++){
            if(cantidadNucleosActivos > 1){
                System.out.print("Pasa por barrera");
                try {
                    barrera.await();
                } catch (InterruptedException ex) {
                    return;
                } catch (BrokenBarrierException ex) {
                    return;
                }
            }
            ++reloj;
            if(idNucleo == 0) {
                System.out.print("\rHilillo corriendo en núcleo" + this.idNucleo + " es " + this.idHililloActual + ", reloj: " + this.reloj);
            }

            if(idNucleo == 1)
                System.out.print(" /// Hilillo corriendo en núcleo" + this.idNucleo + " es " + this.idHililloActual + ", reloj: " + this.reloj + "\r");

            try {
                Thread.sleep(300);
            } catch(InterruptedException e) {
                System.out.println("got interrupted!");
            }
        }
    }
}

/*private boolean intentarBloqueo(){
        boolean respuesta = true;
        if(lockDatosCache0.tryLock() == false){
            respuesta = false;
        }
        if(lockDatosCache1.tryLock() == false){
            respuesta = false;
        }
        if(lockMemoriaDatos.tryLock() == false){
            respuesta = false;
        }
        return respuesta;
    }

    private void desbloquear(){
        lockDatosCache0.unlock();
        lockDatosCache1.unlock();
        lockMemoriaDatos.unlock();
    }*/