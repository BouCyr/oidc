package app.cbo.oidc.java.server.http.staticcontent;

class Data {

    Data() throws IllegalAccessException {
        throw new IllegalAccessException("Static class");
    }

    static final String FAVICO =
            """
                    <svg viewBox="0 0 50 50" xmlns="http://www.w3.org/2000/svg">
                      <defs></defs>
                      <ellipse style="fill: rgb(216, 216, 216); stroke: rgb(0, 0, 0);" cx="25" cy="25" rx="15" ry="20"></ellipse>
                    </svg>
                                        """;


    static final String CSS = """

            html {
                height: 100%;
            }

            body{
              height: 100%;
              font-family: sans-serif;
              font-size: 150%;
              margin: 0;
              background: linear-gradient(223deg,#ff00491f, transparent), linear-gradient(76deg, #dfcd9e,#fff6de);
              color:  black;
              display: flex;
              flex-direction: column;
              align-items: center;
            }

            input, label{
              display:  block;
              margin: auto;
            }

            input{
                border: 0;
                background: rgb(240 211 182 / 49%);
                border-radius: 20px;
                height: 30px;
                font-size: 16px;
                color: black;
                font-family: monospace;
                font-weight: 300;
                padding: 3px 10px 3px 10px;
                margin-bottom: 10px;
                transition: 0.25s;
            }

            input[type="number"]{
                border-radius: 6px;
                display:inline;
                font-size:22px ;
                padding: 3px 0px 3px 10px;
                -moz-appearance: textfield;
            }
            /* Chrome, Safari, Edge, Opera */
            input::-webkit-outer-spin-button,
            input::-webkit-inner-spin-button {
              -webkit-appearance: none;
              margin: 0;
            }

            input[type="submit"]{
                background: rgb(240 211 182);
                width: 100%;
                padding-top: 7px;
                padding-bottom: 30px;
                font-size: 20px;
                font-weight: 500;
                margin-top: 40px;
                margin-bottom: 0px; /* should input:last*/
            }
            input:hover, input:focus{
              background: white;
              outline: 3px solid rgb(240 211 182);
            }

            .FORM {
              box-shadow: 0px 10px 15px -3px rgba(0,0,0,0.1),0px 10px 35px -19px rgba(0,0,0,0.5);
              margin-top: 150px;
              border:  2px solid white;

              background: white;
              backdrop-filter: blur(5px) saturate(1.15);
              border-radius: 20px;
              padding: 60px;
              font-weight: 300;
            }

            h1{
              margin:  0;
              margin-bottom: 20px;
              text-align: center;
              font-weight: 300;
              border-bottom: 1px solid white;
              padding-bottom: 10px;

            }

            """;
}
