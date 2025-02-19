Partial Success, Single Responding Gateway
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <meta http-equiv="content-type" content="text/html;
      charset=windows-1252">
    <title>Initiating Imaging Gateway: Partial Success, Single
      Responding Gateway</title>
  </head>
  <body>
    <h2>Partial Success, Single Responding Gateway</h2>
    <p>Tests the ability of the Initiating Imaging Gateway actor (SUT)
      to respond correctly to a Retrieve Image Document Set (RAD-69)
      Request from an Image Document Consumer actor (Simulator), for two
      DICOM image files, in the case where one of the files is returned
      and the other is unknown to the Image Document Source. The first
      imaging study is present in Community A. The second imaging study
      is requested of Community A but is not present. </p>
    <h3>Retrieve Parameters</h3>
    <table border="1">
      <tbody>
        <tr>
          <td>RIG Home Community ID (A)</td>
          <td>urn:oid:1.3.6.1.4.1.21367.13.70.101</td>
        </tr>
        <tr>
          <td>IDS Repository Unique ID (A1)</td>
          <td>1.3.6.1.4.1.21367.13.71.101</td>
        </tr>
        <tr>
          <td>Transfer Syntax UID</td>
          <td>1.2.840.10008.1.2.1</td>
        </tr>
      </tbody>
    </table>
    <h3>Test Execution</h3>
    <p>The test consists of four steps: </p>
    <ol>
      <li>Test software sends RAD-69 request to System Under test and
        records response.</li>
      <ul>
        <li>System Under Test sends a RAD-75 request to Responding
          Imaging Gateway which stores the request.</li>
        <li>Responding Imaging Gateways provide RAD-75 response to
          System Under Test.</li>
        <li>System Under Test provides RAD-69 response<b> </b>to test
          software.<br>
        </li>
      </ul>
      <li>Test software validates the RAD-75 requests that are sent by
        the System Under Test.</li>
      <li>Test software validates the RAD-69 response sent by the System
        Under Test.</li>
      <li>Test software validates the image returned in the RAD-69
        response to make sure the System Under Test did not alter the
        image.<br>
      </li>
    </ol>
  </body>
</html>
