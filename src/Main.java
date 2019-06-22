public class Main {

    public static void main(String[] args) {
        // Prueba el planificador y PCB
        /*Planificador pla = new Planificador();
        Pcb a = new Pcb("Te");
        Pcb b = new Pcb("Amo");
        Pcb c = new Pcb("Muchisimo");
        Pcb d = new Pcb("Mi");
        Pcb e = new Pcb("Vida");
        pla.setProcesosTerminados(a);
        pla.setProcesosTerminados(b);
        pla.setProcesosTerminados(c);
        pla.setProcesosTerminados(d);
        pla.setProcesosTerminados(e);
        pla.print();*/

        // Prueba la memoria principal
        /*Memoria m = new Memoria();
        int i = 0;
        int[] l = new int[4];

        for(int j = 0; j < 7; ++j){
            for(int k = 0; k < 3; ++k, i += 4){
                m.escribirBloqueDatos(i, l);

                for(int n = 0; n < l.length; ++n){
                    l[n] += 1;
                }
            }
        }
        m.print();*/
        Simulacion simulacion = new Simulacion();
        simulacion.empezarSimulacion();
        System.exit(0);
    }
}
