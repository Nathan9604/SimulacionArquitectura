import java.util.concurrent.TimeoutException;
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

    private CyclicBarrier barrera;
    private ReentrantLock lockDatosCacheLocal;
    private ReentrantLock lockDatosCacheOtro;
    private ReentrantLock lockMemoria;
    private boolean tengoLockLocal;
    private boolean tengoLockOtro;
    private boolean tengoLockMemoria;

    private int idNucleo;

    private int cantidadCiclosHilillo;


    public Nucleo(CacheInstrucciones instrucciones, CacheDatos local, CacheDatos otroCacheDatos, int quantum, Planificador planificador, CyclicBarrier barrera,
                  int idNucleo, ReentrantLock lockDatosCacheLocal, ReentrantLock lockDatosCacheOtro, ReentrantLock lockMemoria){
        this.cacheDatosLocal = local;
        this.otroCacheDatos = otroCacheDatos;
        this.cacheInstrucciones = instrucciones;
        this.registro = new int[32];
        this.instruccionActual = new int[4];
        this.planificador = planificador;
        this.reloj = 0;
        this.quantumTotal = quantum;
        this.barrera = barrera;
        this.lockDatosCacheLocal = lockDatosCacheLocal;
        this.lockDatosCacheOtro = lockDatosCacheOtro;
        this.lockMemoria = lockMemoria;
        this.IR = 0;
        this.idNucleo = idNucleo;
        this.cantidadCiclosHilillo = 0;
        this.tengoLockMemoria = false;
        this.tengoLockOtro = false;
        this.tengoLockMemoria = false;
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
                while(!intentarBloqueo()); // Intente bloquear hasta que lo logre
                cacheDatosLocal.leerDato(registro[instruccion[2]] + instruccion[3], datos);
                desbloquear(); // Desbloquea los recursos de la simulación
                registro[instruccion[1]] = datos[0];
                numCiclos = datos[1];
                break;

            case 37: // sw
                // Si el RL del otro núcleo tiene la dirección que voy a usar lo inválido
                if(otroCacheDatos.getRl() == (registro[instruccion[1]] + instruccion[3]))
                    otroCacheDatos.setRl(-1);

                while(!intentarBloqueo()); // Intente bloquear hasta que lo logre
                numCiclos = cacheDatosLocal.escribirDato(registro[instruccion[1]] + instruccion[3], registro[instruccion[2]]);
                desbloquear(); // Desbloquea los recursos de la simulación
                break;

            case 99: // beq
                if(registro[instruccion[1]] == registro[instruccion[2]])
                    PC += 4 * instruccion[3];
                break;

            case 100: //bne
                if(registro[instruccion[1]] != registro[instruccion[2]])
                    PC += 4 * instruccion[3];
                break;

            case 51: // lr
                datos = new int[2];

                // Si el RL del otro núcleo tiene la dirección que voy a usar lo inválido
                if(otroCacheDatos.getRl() == registro[instruccion[2]])
                    otroCacheDatos.setRl(-1);

                while(!intentarBloqueo()); // Intente bloquear hasta que lo logre
                cacheDatosLocal.leerDato(registro[instruccion[2]], datos);
                desbloquear(); // Desbloquea los recursos de la simulación
                registro[instruccion[1]] = datos[0];
                numCiclos = datos[1];
                cacheDatosLocal.setRl(registro[instruccion[2]]);
                break;

            case 52: // sc
                if(cacheDatosLocal.getRl() == registro[instruccion[1]]) {
                    // Si el RL del otro núcleo tiene la dirección que voy a usar lo inválido
                    if(otroCacheDatos.getRl() == registro[instruccion[1]])
                        otroCacheDatos.setRl(-1);

                    while(!intentarBloqueo()); // Intente bloquear hasta que lo logre
                    numCiclos = cacheDatosLocal.escribirDato(registro[instruccion[1]], registro[instruccion[2]]);
                    desbloquear(); // Desbloquea los recursos de la simulación
                }
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
        planificador.agregarNucleoActivo();

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

        planificador.desactivarNucleoActivo();
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
            if(planificador.getCantidadNucleosActivos() > 1){
                try {
                    barrera.await(10L, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    return;
                } catch (BrokenBarrierException ex) {
                    return;
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }

            ++reloj;
            if(idNucleo == 0) {
                System.out.print("\rHilillo corriendo en núcleo" + this.idNucleo + " es " + this.idHililloActual + ", reloj: " + this.reloj);
            }

            if(idNucleo == 1)
                System.out.print(" /// Hilillo corriendo en núcleo" + this.idNucleo + " es " + this.idHililloActual + ", reloj: " + this.reloj + "\r");

            /*try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                System.out.println("got interrupted!");
            }*/
        }
    }

    private boolean intentarBloqueo(){
        boolean bloqueoCorrecto = true;

        tengoLockLocal = lockDatosCacheLocal.tryLock();
        if(!tengoLockLocal){
            desbloquear();
            bloqueoCorrecto = false;
        }

        if(bloqueoCorrecto) {
            tengoLockOtro = lockDatosCacheOtro.tryLock();
            if (!tengoLockOtro) {
                desbloquear();
                bloqueoCorrecto = false;
            }
        }

        if(bloqueoCorrecto) {
            tengoLockMemoria = lockMemoria.tryLock();
            if (!tengoLockMemoria) {
                desbloquear();
                bloqueoCorrecto = false;
            }
        }

        return bloqueoCorrecto;
    }

    private void desbloquear(){
        if(tengoLockLocal) {
            lockDatosCacheLocal.unlock();
            tengoLockLocal = false;
        }

        if(tengoLockOtro) {
            lockDatosCacheOtro.unlock();
            tengoLockOtro = false;
        }

        if(tengoLockMemoria) {
            lockMemoria.unlock();
            tengoLockMemoria = false;
        }
    }
}