package com.github.ptracker.interchange;

public interface DataInterchange<FROM, TO> {

  TO convertForward(FROM obj);

  FROM convertBackward(TO obj);
}
