import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.lang.*;

public class Nucleo extends Thread {
    private Pcb pcb;   // PCB del hilillo en el núcleo
    private int registro[];
    private int PC;
    private int RL;
    private int IR;
    private char estadoHililloActual; // Esta procesando o ya finalizo ?
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
    private boolean tengoLockLocal;     // booleano para saber si tengo acceso concedido a mi caché
    private boolean tengoLockOtro;      // booleano para saber si tengo acceso concedido a la otra caché
    private boolean tengoLockMemoria;   // booleano para saber si tengo acceso concedido a la memoria

    private int idNucleo;

    private int cantidadCiclosHilillo;

    private int cantidadVecesHilillo[]; // Para medir la cantidad de veces que corrió cada hilillo en en nucleo

    private int solicitudesAccesoMemoria; // Para llevar cuenta de todas las solicitudes a memoria

    private int totalFallosCache; // Lleva cuenta de los fallos de cache que se producen


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
        this.cantidadVecesHilillo = new int[7];
        this.solicitudesAccesoMemoria = 0;
        this.totalFallosCache = 0;

    }

    /**
     * Decofidica, ejecuta y realiza operaciones de memoria
     * @param instruccion es un arreglo que posee los 4 operandos del a instrucción
     */
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
                datos = new int[2]; // En la posición 0 estará el dato en la 1 el número de ciclos de la operación
                while(!intentarBloqueo()); // Intente bloquear hasta que lo logre
                cacheDatosLocal.leerDato(registro[instruccion[2]] + instruccion[3], datos);
                this.solicitudesAccesoMemoria++; // Se suma acceso a memoria a estadísticas
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
                this.solicitudesAccesoMemoria++; // Se suma acceso a memoria a estadísticas
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
                datos = new int[2]; // En la posición 0 estará el dato en la 1 el número de ciclos de la operación

                // Si el RL del otro núcleo tiene la dirección que voy a usar lo inválido
                if(otroCacheDatos.getRl() == registro[instruccion[2]])
                    otroCacheDatos.setRl(-1);

                while(!intentarBloqueo()); // Intente bloquear hasta que lo logre
                cacheDatosLocal.leerDato(registro[instruccion[2]], datos);
                this.solicitudesAccesoMemoria++; // Se suma acceso a memoria a estadísticas
                desbloquear(); // Desbloquea los recursos de la simulación
                registro[instruccion[1]] = 0;//datos[0];
                numCiclos = datos[1];
                cacheDatosLocal.setRl(registro[instruccion[2]]); // Establece el valor del RL
                break;

            case 52: // sc
                if(cacheDatosLocal.getRl() == registro[instruccion[1]]) {
                    // Si el RL del otro núcleo tiene la dirección que voy a usar lo inválido
                    if(otroCacheDatos.getRl() == registro[instruccion[1]])
                        otroCacheDatos.setRl(-1);

                    while(!intentarBloqueo()); // Intente bloquear hasta que lo logre
                    numCiclos = cacheDatosLocal.escribirDato(registro[instruccion[1]], registro[instruccion[2]]);
                    this.solicitudesAccesoMemoria++; // Se suma acceso a memoria a estadísticas
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

        if(numCiclos > 4){ // Si hay más de esta cantidad de ciclos, hubo fallo de caché
            this.totalFallosCache++;
        }

        this.cantidadCiclosHilillo += numCiclos; // Se le suma cantidad de ciclos de reloj que lleva el hilillo
    }

    /**
     * Método inicial del hilo. En el se hacen los cambios de contexto y cada una de las instrucciones
     */
    public void run(){
        planificador.agregarNucleoActivo(); // Hay un nuevo núcleo

        while (planificador.hayHilillo()){  // Mientras haya hilillos que procesar
            cargarContexto(); // cargue el contexto del hilillo

            // Mientras tenga quantum y el hilillo no haya acabado
            while (estadoHililloActual == 'R' && this.quantumHililloActual > 0) {
                obtenerSiguienteInstruccion(); // Haga fetch de la siguiente instrucción
                IR = PC;
                PC += 4;
                decodificador(instruccionActual); // Decodifiquela y ejecutela a la vez
                --this.quantumHililloActual;
            }

            guardarContexto(); // Guarde el contexto de este PCB
        }

        // Ponga candados antes de indicar que el núcleo acabo para evitar problemas de sincronización
        // con la barrera
        planificador.ponerCandadoNucleosActivos();
        planificador.desactivarNucleoActivo();
        planificador.liberarCandadoNucleosActivos();

        System.out.println("Accesos a memoria del núcleo" + idNucleo + " es: " + this.solicitudesAccesoMemoria + "\n");

        System.out.println("Fallos del núcleo" + idNucleo + " es: " + this.totalFallosCache + "\n");

        double accesos = this.solicitudesAccesoMemoria;

        double fallos = this.totalFallosCache;

        double tasaFallos = fallos / accesos;

        System.out.println("La tasa de fallos del núcleo " + idNucleo + " es: " + tasaFallos + "\n");

        System.out.println("La cantidad de veces que corren los hilillos en el nucleo " + idNucleo + " es: " + "\n" +
                "0: " + this.cantidadVecesHilillo[0] + "\n" +
                "1: " + this.cantidadVecesHilillo[1] + "\n" +
                "2: " + this.cantidadVecesHilillo[2] + "\n" +
                "3: " + this.cantidadVecesHilillo[3] + "\n" +
                "4: " + this.cantidadVecesHilillo[4] + "\n" +
                "5: " + this.cantidadVecesHilillo[5] + "\n" +
                "6: " + this.cantidadVecesHilillo[6] + "\n");

    }

    /**
     * Se almacenan todas las variables modificadas a lo largo del quantum en el PCB el cual es distinto para
     * cada uno de los hilillos
     */
    private void guardarContexto() {
        this.pcb.setPc(this.PC);
        this.pcb.setIr(this.IR);
        this.pcb.setEstado(this.estadoHililloActual);
        this.pcb.setRegistro(registro);
        int ciclos = this.pcb.getCiclosReloj();
        ciclos += this.cantidadCiclosHilillo;
        this.pcb.setCiclosReloj(ciclos);

        // Si el estado del hilillo es F significa que el hilillo ya acabo, si es R es que le resta
        // tiempo de CPU
        if(this.estadoHililloActual == 'R')
            planificador.agregarProcesosRestantes(this.pcb);
        else
            planificador.agregarProcesosTerminados(this.pcb);
    }

    /**
     * Carga el contexto de un proceso o hilillo en el núcleo
     */
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

        char hilillo = this.idHililloActual.charAt(0);

        int indice = Character.getNumericValue(hilillo);

        switch(indice) {
            case 0:
                this.cantidadVecesHilillo[indice]++;
                break;
            case 1:
                this.cantidadVecesHilillo[indice]++;
                break;
            case 2:
                this.cantidadVecesHilillo[indice]++;
                break;
            case 3:
                this.cantidadVecesHilillo[indice]++;
                break;
            case 4:
                this.cantidadVecesHilillo[indice]++;
                break;
            case 5:
                this.cantidadVecesHilillo[indice]++;
                break;
            case 6:
                this.cantidadVecesHilillo[indice]++;
                break;
        }
    }

    /**
     * Fetch, lee la siguiente instrucción en la memoria y la carga en un vector de instrucción
     */
    private void obtenerSiguienteInstruccion(){
        int numCiclos = cacheInstrucciones.leerInstruccion(PC, instruccionActual);

        cicloReloj(numCiclos);
    }

    /**
     * Realiza todos los calculos matematicos de las operaciones en las instrucciones aritméticas
     * @param operacion Código de operación que nos indica la operación que se debe realizar
     * @param operando1 Operando 1 de la operación a realizar
     * @param operando2 Operando 2 de la operación a realizar
     * @return resultado el a operación aritmética
     */
    private int Alu(int operacion, int operando1, int operando2){
        int resultado = -1;

        switch(operacion){
            case 1: // Suma
                resultado = operando1 + operando2;
                break;
            case 2: // Resta
                resultado = operando1 - operando2;
                break;
            case 3: // Multiplicación
                resultado = operando1 * operando2;
                break;
            case 4: // División
                resultado = operando1 / operando2;
                break;
        }
        return resultado;
    }

    /**
     * Posee la barrera utilizada para sincronizar ambos núcleos, además de eso refresca los datos desplegados en pantalla
     * @param numCiclos Número de ciclos quetardo la operación en realizarse
     */
    public void cicloReloj(int numCiclos){
        for(int i = 0; i < numCiclos; i++){
            // Si hay más de 1 núcleo espere en la barrera
            if(planificador.getCantidadNucleosActivos() > 1){
                try {
                    barrera.await(2L, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    return;
                } catch (BrokenBarrierException ex) {
                    return;
                } catch (TimeoutException e) {

                }
            }

            // Refrescamiento de los datos en la vista
            ++reloj;
            if(idNucleo == 0) {
                System.out.print("\rHilillo corriendo en núcleo" + this.idNucleo + " es " + this.idHililloActual + ", reloj: " + this.reloj);
            }

            if(idNucleo == 1)
                System.out.print(" /// Hilillo corriendo en núcleo" + this.idNucleo + " es " + this.idHililloActual + ", reloj: " + this.reloj + "\r");

            // Pequeño retraso para visualizar los cambios en la vista
            try {
                Thread.sleep(50);
            } catch(InterruptedException e) {
            }
        }
    }

    /**
     * Intenta bloquear cada uno de los recursos de la simulación, es decir ambas cachés de instrucciones
     * y la memoria principal. Si no puede bloquear 1 libera todas e intenta de nuevo
     * @return true si pudo bloquear todos los recursos y false en caso contrario
     */
    private boolean intentarBloqueo(){
        boolean bloqueoCorrecto = true;

        // Intente bloquear el caché local
        tengoLockLocal = lockDatosCacheLocal.tryLock();
        if(!tengoLockLocal){
            desbloquear();
            bloqueoCorrecto = false;
        }

        // Intente bloquear el otro caché de datos
        if(bloqueoCorrecto) {
            tengoLockOtro = lockDatosCacheOtro.tryLock();
            if (!tengoLockOtro) {
                desbloquear();
                bloqueoCorrecto = false;
            }
        }

        // Intente bloquear la memoria principal
        if(bloqueoCorrecto) {
            tengoLockMemoria = lockMemoria.tryLock();
            if (!tengoLockMemoria) {
                desbloquear();
                bloqueoCorrecto = false;
            }
        }

        return bloqueoCorrecto;
    }

    /**
     * Desbloquea cada uno de los recursos que tenga el hilo bloqueado, solo desbloqueará los que bloqueo para
     * evitar problemas de sincronización
     */
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